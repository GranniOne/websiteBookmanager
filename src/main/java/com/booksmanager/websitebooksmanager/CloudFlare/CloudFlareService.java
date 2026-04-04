package com.booksmanager.websitebooksmanager.CloudFlare;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudFlareService {

    public Map<String, DataStruct> signedUrls = new HashMap<String, DataStruct>();

    private final CloudflareR2Client cloudflareR2Client;

    public CloudFlareService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    public void createPresignedurlBooks(String[] parameters, DataTypes type, String directory) {
        String path = parameters[0] +  "/" + parameters[1] + "/" + parameters[2];

            if(signedUrls.containsKey(directory)) {

            }else{
                DataStruct test = new DataStruct();
                switch (type) {
                    case MetaData:
                        test.setPresignedMetaData(generatePresignedDownloadUrl(path));
                        signedUrls.put(directory,test);
                        break;
                    case PDF:
                        test.setPresignedPdf(generatePresignedDownloadUrl(path));
                        signedUrls.put(directory,test);
                        break;
                    case COVER:
                        test.setPresignedCover(generatePresignedDownloadUrl(path));
                        signedUrls.put(directory,test);
                        System.out.println(test.getPresignedCover());
                        break;
                    default:
                }
            }



    }

    private String generatePresignedDownloadUrl(String parameters) {
        return cloudflareR2Client.generatePresignedDownloadUrl(
                "bookmanager",
                parameters,
                Duration.ofMinutes(15)
        );
    }




}
