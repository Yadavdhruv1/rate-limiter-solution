package ratelimiter;

public class Request {

    private final String customerId;
    private final String tenantId;
    private final String apiKey;
    private final String provider;

    public Request(String customerId, String tenantId, String apiKey, String provider) {
        this.customerId = customerId;
        this.tenantId = tenantId;
        this.apiKey = apiKey;
        this.provider = provider;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getProvider() {
        return provider;
    }
}
