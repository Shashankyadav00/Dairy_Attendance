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
    // GET PAYMENT STATUS (USER SPECIFIC)
    // -------------------------------------------------------
    @GetMapping("/{shift}")
    @Transactional
    public Map<String, Object> getPaymentsByShift(
            @PathVariable String shift,
            @RequestParam Long userId
    ) {

        Map<String, Object> resp = new HashMap<>();
        LocalDate today = LocalDate.now();

        try {
            // Load only user-specific customers
            List<Customer> customers = customerRepository.findByShiftAndUserId(shift, userId);

            // Create today's payment record if missing
            for (Customer c : customers) {
                String name = c.getFullName() != null ? c.getFullName() : c.getNickname();
                if (name == null || name.isBlank()) continue;

                if (paymentRepository.findAllMatchingForUser(shift, today, name, userId).isEmpty()) {
                    Payment p = new Payment();
                    p.setCustomerName(name);
                    p.setShift(shift);
                    p.setPaid(false);
                    p.setDate(today);
                    p.setUserId(userId);
                    paymentRepository.save(p);
                }
            }

            List<Payment> payments =
                    paymentRepository.findByShiftAndDateAndUserId(shift, today, userId);

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
    // SAVE PAYMENT STATUS (USER SPECIFIC)
    // -------------------------------------------------------
    @PostMapping
    @Transactional
    public Map<String, Object> savePayment(@RequestBody Map<String, Object> body) {

        Map<String, Object> resp = new HashMap<>();

        try {
            String customerName = Objects.toString(body.get("customerName"), "");
            String shift = Objects.toString(body.get("shift"), "");
            boolean paid = Boolean.parseBoolean("" + body.get("paid"));
            Long userId = Long.parseLong("" + body.get("userId"));

            LocalDate today = LocalDate.now();

            List<Payment> existing =
                    paymentRepository.findAllMatchingForUser(shift, today, customerName, userId);

            Payment p = existing.isEmpty() ? new Payment() : existing.get(0);

            p.setCustomerName(customerName);
            p.setShift(shift);
            p.setPaid(paid);
            p.setDate(today);
            p.setUserId(userId);

            resp.put("success", true);
            resp.put("payment", paymentRepository.save(p));

        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", "Failed to save payment: " + e.getMessage());
        }

        return resp;
    }

    // -------------------------------------------------------
    // CRON — CHECK EVERY MINUTE
    // -------------------------------------------------------
    @Scheduled(cron = "0 * * * * *")
    public void checkReminder() {
        System.out.println("⏰ Scheduler tick: " + LocalTime.now());
    }

    // -------------------------------------------------------
    // SEND EMAIL — USER SPECIFIC
    // -------------------------------------------------------
    private void sendShiftEmail(String shift) {

        try {
            LocalDate today = LocalDate.now();

            // Admin-level email: send all unpaid customers (all users)
            List<Payment> unpaid = paymentRepository
                    .findByShiftAndPaidFalseAndDate(shift, today);

            if (unpaid.isEmpty()) return;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(reminderEmail);
            helper.setSubject("Unpaid Customers (" + shift + ") - " + today);

            StringBuilder html = new StringBuilder();
            html.append("<h2>Unpaid Customers — ").append(shift).append(" Shift</h2>");

            html.append("<table border='1' cellpadding='8' cellspacing='0'>");
            html.append("<tr><th>Name</th><th>Litres</th><th>Rate</th><th>Total</th></tr>");

            for (Payment p : unpaid) {

                Optional<MilkEntry> entry =
                        milkEntryRepository.findByUserIdAndShiftAndDateAndCustomerName(
                                p.getUserId(), shift, today, p.getCustomerName()
                        );

                double litres = entry.map(MilkEntry::getLitres).orElse(0.0);
                double rate = entry.map(MilkEntry::getRate).orElse(0.0);
                double total = litres * rate;

                html.append("<tr>")
                        .append("<td>").append(p.getCustomerName()).append("</td>")
                        .append("<td>").append(litres).append("</td>")
                        .append("<td>").append(rate).append("</td>")
                        .append("<td><b>").append(total).append("</b></td>")
                        .append("</tr>");
            }

            html.append("</table>");

            helper.setText(html.toString(), true);
            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
