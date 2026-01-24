package com.travolish.traveller.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookUserData {
    private String id;
    private String name;
    private String email;
    private String picture;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Picture {
        private Data data;
        
        @lombok.Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Data {
            private String url;
        }
    }
}
