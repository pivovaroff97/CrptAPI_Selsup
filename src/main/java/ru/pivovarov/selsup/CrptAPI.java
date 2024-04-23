package ru.pivovarov.selsup;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptAPI {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private int requestCount;
    private long startTime;
    private final Lock lock = new ReentrantLock();

    public CrptAPI(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        startTime = System.currentTimeMillis();
    }

    public HttpResponse<String> createDocument(Document document, String sign) throws URISyntaxException, IOException, InterruptedException {
        DocumentWithSign documentWithSign = DocumentWithSign.builder()
                .document(document)
                .sign(sign).build();
        ObjectMapper objectMapper = new ObjectMapper();
        String documentWithSignJSON = objectMapper.writeValueAsString(documentWithSign);
        HttpResponse<String> response;
        lock.lock();
        try {
            long curTimeMillis = System.currentTimeMillis();
            if (curTimeMillis - startTime >= timeUnit.toMillis(1)) {
                requestCount = 0;
                startTime = curTimeMillis;
            }
            if (requestCount >= requestLimit) {
                Thread.sleep(curTimeMillis - (timeUnit.toMillis(1) + startTime));
                requestCount = 0;
                startTime = System.currentTimeMillis();
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(documentWithSignJSON))
                    .build();
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            requestCount++;
        } finally {
            lock.unlock();
        }
        return response;
    }
}

@Builder
@Data
class DocumentWithSign {
    public Document document;
    public String sign;
}

@Builder
@Data
class Description{
    public String participantInn;
}

@Builder
@Data
class Product{
    public String certificate_document;
    public String certificate_document_date;
    public String certificate_document_number;
    public String owner_inn;
    public String producer_inn;
    public String production_date;
    public String tnved_code;
    public String uit_code;
    public String uitu_code;
}

@Builder
@Data
class Document{
    public Description description;
    public String doc_id;
    public String doc_status;
    public String doc_type;
    public boolean importRequest;
    public String owner_inn;
    public String participant_inn;
    public String producer_inn;
    public String production_date;
    public String production_type;
    public ArrayList<Product> products;
    public String reg_date;
    public String reg_number;
}
