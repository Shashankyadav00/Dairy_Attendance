package com.milkattendence.backend.controller;

import com.milkattendence.backend.model.Customer;
import com.milkattendence.backend.model.MilkEntry;
import com.milkattendence.backend.model.Payment;
import com.milkattendence.backend.repository.CustomerRepository;
import com.milkattendence.backend.repository.MilkEntryRepository;
import com.milkattendence.backend.repository.PaymentRepository;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final MilkEntryRepository milkEntryRepository;

    private JavaMailSender mailSender;

    @Value("${app.reminder.email:}")
    private String reminderEmail;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    // MORNING REMINDER SETTINGS
    private volatile boolean reminderEnabledMorning = false;
    private volatile String morningReminderTime = "08:00";
    private volatile LocalDate reminderStartMorning = null;
    private volatile int reminderDurationDaysMorning = 0;

    // NIGHT REMINDER SETTINGS
    private volatile boolean reminderEnabledNight = false;
    private volatile String nightReminderTime = "20:00";
    private volatile LocalDate reminderStartNight = null;
    private volatile int reminderDurationDaysNight = 0;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository,
                             CustomerRepository customerRepository,
                             MilkEntryRepository milkEntryRepository) {
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.milkEntryRepository = milkEntryRepository;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // -------------------------------------------------------
    // GET PAYMENT STATUS
    // -------------------------------------------------------
    @GetMapping("/{shift}")
    @Transactional
    public Map<String, Object> getPaymentsByShift(@PathVariable String shift) {

        Map<String, Object> resp = new HashMap<>();
        LocalDate today = LocalDate.now();

        try {
            List<Customer> customers = Optional.ofNullable(customerRepository.findByShift(shift))
                    .orElse(Collections.emptyList());

            // Create today's record if missing
            for (Customer c : customers) {
                String name = c.getFullName() != null ? c.getFullName() : c.getNickname();
                if (name == null || name.isBlank()) continue;

                if (paymentRepository.findAllMatching(shift, today, name).isEmpty()) {
                    Payment p = new Payment();
                    p.setCustomerName(name);
                    p.setShift(shift);
                    p.setPaid(false);
                    p.setDate(today);
                    paymentRepository.save(p);
                }
            }

            List<Payment> payments = paymentRepository.findByShiftAndDate(shift, today);
            payments.sort(Comparator.comparing(Payment::getCustomerName));

            resp.put("success", true);
            resp.put("payments", payments);

        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
        }

        return resp;
    }

    // -------------------------------------------------------
    // SAVE PAYMENT STATUS
    // -------------------------------------------------------
    @PostMapping
    @Transactional
    public Map<String, Object> savePayment(@RequestBody Map<String, Object> body) {

        Map<String, Object> resp = new HashMap<>();

        try {
            String customerName = Objects.toString(body.get("customerName"), "");
            String shift = Objects.toString(body.get("shift"), "");
            boolean paid = Boolean.parseBoolean("" + body.get("paid"));

            LocalDate today = LocalDate.now();

            List<Payment> existing = paymentRepository.findAllMatching(shift, today, customerName);
            Payment p = existing.isEmpty() ? new Payment() : existing.get(0);

            p.setCustomerName(customerName);
            p.setShift(shift);
            p.setPaid(paid);
            p.setDate(today);

            resp.put("success", true);
            resp.put("payment", paymentRepository.save(p));

        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", "Failed to save payment: " + e.getMessage());
        }

        return resp;
    }

    // -------------------------------------------------------
    // SAVE REMINDER SETTINGS
    // -------------------------------------------------------
    @PostMapping("/save-reminder")
    public Map<String, Object> saveReminder(@RequestBody Map<String, Object> body) {

        try {
            String shift = Objects.toString(body.get("shift"), "Morning");
            String time = Objects.toString(body.get("time"), "08:00");
            boolean enabled = Boolean.parseBoolean("" + body.get("enabled"));
            int durationDays = Integer.parseInt("" + body.get("durationDays"));

            if (shift.equalsIgnoreCase("Morning")) {
                morningReminderTime = time;
                reminderEnabledMorning = enabled;
                reminderDurationDaysMorning = durationDays;
                reminderStartMorning = enabled ? LocalDate.now() : null;

            } else {
                nightReminderTime = time;
                reminderEnabledNight = enabled;
                reminderDurationDaysNight = durationDays;
                reminderStartNight = enabled ? LocalDate.now() : null;
            }

            return Map.of(
                    "success", true,
                    "morningTime", morningReminderTime,
                    "nightTime", nightReminderTime,
                    "enabledMorning", reminderEnabledMorning,
                    "enabledNight", reminderEnabledNight
            );

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // -------------------------------------------------------
    // GET REMINDER SETTINGS
    // -------------------------------------------------------
    @GetMapping("/reminder-times")
    public Map<String, Object> getReminderTimes() {
        return Map.of(
                "success", true,
                "morning", morningReminderTime,
                "night", nightReminderTime,
                "enabledMorning", reminderEnabledMorning,
                "enabledNight", reminderEnabledNight
        );
    }

    // -------------------------------------------------------
    // CRON — CHECK EVERY MINUTE
    // -------------------------------------------------------
   @Scheduled(cron = "0 * * * * *")
public void checkReminder() {

    // ⭐ ADD THIS LINE
    System.out.println("⏰ Scheduler tick: " + LocalTime.now());

    String now = LocalTime.now().withSecond(0).toString().substring(0, 5);

    if (reminderEnabledMorning && now.equals(morningReminderTime)) {
        sendShiftEmail("Morning");
    }

    if (reminderEnabledNight && now.equals(nightReminderTime)) {
        sendShiftEmail("Night");
    }
}


    // -------------------------------------------------------
    // SEND EMAIL WITH LITRES + RATE + TOTAL
    // -------------------------------------------------------
    private void sendShiftEmail(String shift) {

        try {
            LocalDate today = LocalDate.now();

            List<Payment> unpaid = paymentRepository
                    .findByShiftAndPaidFalseAndDate(shift, today);

            if (unpaid.isEmpty()) return;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(reminderEmail);
            helper.setSubject("Unpaid Customers (" + shift + ") - " + today);

            // Build HTML table
            StringBuilder html = new StringBuilder();
            html.append("<h2>Unpaid Customers — ").append(shift).append(" Shift</h2>");
            html.append("<p>Date: ").append(today).append("</p>");

            html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
            html.append("<tr style='background:#f0f0f0;'>");
            html.append("<th>Name</th><th>Litres</th><th>Rate</th><th>Total</th>");
            html.append("</tr>");

            for (Payment p : unpaid) {

                Optional<MilkEntry> entry =
                        milkEntryRepository.findByShiftAndDateAndCustomerName(
                                shift, today, p.getCustomerName()
                        );

                double litres = entry.map(MilkEntry::getLitres).orElse(0.0);
                double rate = entry.map(MilkEntry::getRate).orElse(0.0);
                double total = litres * rate;

                html.append("<tr>")
                        .append("<td>").append(p.getCustomerName()).append("</td>")
                        .append("<td>").append(String.format("%.2f", litres)).append("</td>")
                        .append("<td>₹").append(String.format("%.2f", rate)).append("</td>")
                        .append("<td><b>₹").append(String.format("%.2f", total)).append("</b></td>")
                        .append("</tr>");
            }

            html.append("</table>");
            html.append("<p style='margin-top:10px;'>Please collect payment from the above customers.</p>");

            helper.setText(html.toString(), true);
            mailSender.send(message);

            System.out.println("📨 Sent reminder email for shift " + shift);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
