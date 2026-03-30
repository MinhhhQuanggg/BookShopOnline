package fit.hutech.spring.services;

import fit.hutech.spring.entities.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${app.payment.vnpay.enabled:false}")
    private boolean vnpayEnabled;

    @Value("${app.payment.vnpay.tmn-code:}")
    private String vnpTmnCode;

    @Value("${app.payment.vnpay.hash-secret:}")
    private String vnpHashSecret;

    @Value("${app.payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${app.payment.vnpay.return-url:}")
    private String vnpReturnUrl;

    public boolean isConfigured() {
        return vnpayEnabled
                && vnpTmnCode != null && !vnpTmnCode.isBlank()
                && vnpHashSecret != null && !vnpHashSecret.isBlank();
    }

    public String createPaymentUrl(Invoice invoice, HttpServletRequest request) {
        if (!isConfigured()) {
            return "/payments/mock/" + invoice.getId();
        }

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", String.valueOf(Math.round((invoice.getPrice() == null ? 0D : invoice.getPrice()) * 100)));
        params.put("vnp_BankCode", "");
        params.put("vnp_Command", "pay");
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_IpAddr", request.getRemoteAddr() == null ? "127.0.0.1" : request.getRemoteAddr());
        params.put("vnp_Locale", "vn");
        params.put("vnp_OrderInfo", "Thanh toan hoa don #" + invoice.getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", getReturnUrl(request));
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_TxnRef", String.valueOf(invoice.getId()));
        params.put("vnp_Version", "2.1.0");

        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (query.length() > 0) {
                query.append('&');
                hashData.append('&');
            }
            String encodedKey = encode(entry.getKey());
            String encodedValue = encode(entry.getValue());
            query.append(encodedKey).append('=').append(encodedValue);
            hashData.append(encodedKey).append('=').append(encodedValue);
        }

        String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        return vnpPayUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public PaymentReturn parseVnpayReturn(Map<String, String[]> rawParams) {
        Map<String, String> params = new TreeMap<>();
        rawParams.forEach((key, values) -> {
            if (key == null || key.isBlank()) {
                return;
            }
            if ("vnp_SecureHash".equalsIgnoreCase(key) || "vnp_SecureHashType".equalsIgnoreCase(key)) {
                return;
            }
            if (values != null && values.length > 0 && values[0] != null) {
                params.put(key, values[0]);
            }
        });

        String secureHash = firstValue(rawParams, "vnp_SecureHash");
        String hashData = buildHashData(params);
        boolean validSignature = secureHash != null && secureHash.equalsIgnoreCase(hmacSHA512(vnpHashSecret, hashData));

        Long invoiceId = parseLong(firstValue(rawParams, "vnp_TxnRef"));
        String transactionNo = firstValue(rawParams, "vnp_TransactionNo");
        String responseCode = firstValue(rawParams, "vnp_ResponseCode");
        String transactionStatus = firstValue(rawParams, "vnp_TransactionStatus");
        boolean success = validSignature && "00".equals(responseCode) && "00".equals(transactionStatus);
        String message = success ? "success" : "failed";
        return new PaymentReturn(invoiceId, transactionNo, responseCode, transactionStatus, success, message, validSignature);
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (hashData.length() > 0) {
                hashData.append('&');
            }
            hashData.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return hashData.toString();
    }

    private String getReturnUrl(HttpServletRequest request) {
        if (vnpReturnUrl != null && !vnpReturnUrl.isBlank()) {
            return vnpReturnUrl;
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        StringBuilder base = new StringBuilder();
        base.append(scheme).append("://").append(host);
        if (("http".equalsIgnoreCase(scheme) && port != 80) || ("https".equalsIgnoreCase(scheme) && port != 443)) {
            base.append(":").append(port);
        }
        base.append(context).append("/payments/vnpay/return");
        return base.toString();
    }

    private String firstValue(Map<String, String[]> params, String key) {
        String[] values = params.get(key);
        return values == null || values.length == 0 ? null : values[0];
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII).replace("+", "%20");
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot compute payment signature", ex);
        }
    }

    public record PaymentReturn(Long invoiceId,
                                String transactionNo,
                                String responseCode,
                                String transactionStatus,
                                boolean success,
                                String message,
                                boolean signatureValid) {
    }
}