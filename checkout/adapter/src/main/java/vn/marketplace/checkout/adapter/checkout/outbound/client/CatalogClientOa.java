package vn.marketplace.checkout.adapter.checkout.outbound.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import vn.marketplace.checkout.application.checkout.CatalogPort;

/**
 * Outbound client to Catalog ({@code Catalog.GetPrice}). The SAD specifies gRPC + mTLS; this REST
 * client is the transport stand-in, consistent with the sibling services' internal endpoints.
 */
@Component
public class CatalogClientOa implements CatalogPort {

    private final RestClient rest;

    public CatalogClientOa(@Value("${checkout.clients.catalog-url:http://localhost:8080}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public List<SkuPriceDto> fetchPrices(List<String> skuCodes) {
        Envelope<List<PriceLine>> envelope = rest.post()
                .uri("/internal/prices")
                .body(new GetPriceRequest(skuCodes))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return envelope.data().stream()
                .map(p -> new SkuPriceDto(p.skuCode(), p.priceAmount(), p.currency(), p.merchantId(),
                        p.active()))
                .toList();
    }

    record GetPriceRequest(List<String> skuCodes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PriceLine(String skuCode, long priceAmount, String currency, String merchantId,
                     boolean active) {
    }
}
