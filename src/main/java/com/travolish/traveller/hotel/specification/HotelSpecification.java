package com.travolish.traveller.hotel.specification;

import org.springframework.data.jpa.domain.Specification;

import com.travolish.traveller.hotel.model.Hotel;

public class HotelSpecification {

    public static Specification<Hotel> withCountry(String country) {
        return (root, query, criteriaBuilder) ->
                country == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("country")),
                        country.toLowerCase());
    }

    public static Specification<Hotel> withCity(String city) {
        return (root, query, criteriaBuilder) ->
                city == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("city")),
                        city.toLowerCase());
    }

    public static Specification<Hotel> withName(String name) {
        return (root, query, criteriaBuilder) ->
                name == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%");
    }

    public static Specification<Hotel> withMinRating(Double minRating) {
        return (root, query, criteriaBuilder) ->
                minRating == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    public static Specification<Hotel> withMaxRating(Double maxRating) {
        return (root, query, criteriaBuilder) ->
                maxRating == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.lessThanOrEqualTo(root.get("rating"), maxRating);
    }

    public static Specification<Hotel> withBbox(Double latMin, Double latMax, Double lngMin, Double lngMax) {
        return (root, query, cb) -> {
            if (latMin == null || latMax == null || lngMin == null || lngMax == null)
                return cb.conjunction();
            return cb.and(
                cb.greaterThanOrEqualTo(root.get("latitude"), latMin),
                cb.lessThanOrEqualTo(root.get("latitude"), latMax),
                cb.greaterThanOrEqualTo(root.get("longitude"), lngMin),
                cb.lessThanOrEqualTo(root.get("longitude"), lngMax)
            );
        };
    }

    public static Specification<Hotel> withQuery(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) return cb.conjunction();
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("city")), pattern)
            );
        };
    }

    public static Specification<Hotel> withStatus(Hotel.HotelStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

}
