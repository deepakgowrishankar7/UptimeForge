package com.pulsewatch.dto;

public class SendOtpResponse {

    private String email;
    private String previewOtp;
    private String message;

    public SendOtpResponse() {}

    public SendOtpResponse(String email, String previewOtp, String message) {
        this.email = email;
        this.previewOtp = previewOtp;
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPreviewOtp() {
        return previewOtp;
    }

    public void setPreviewOtp(String previewOtp) {
        this.previewOtp = previewOtp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
