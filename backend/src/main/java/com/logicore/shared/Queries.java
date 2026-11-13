package com.logicore.shared;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

public class Queries {

  public static Pageable page(int page, int size) {
    return PageRequest.of(
      Math.max(0, page),
      Math.min(100, Math.max(1, size)),
      Sort.by(Sort.Direction.DESC, "createdAt")
    );
  }

  public static <T> Specification<T> search(String q, String... fields) {
    return (r, c, b) -> {
      if (q == null || q.isBlank()) return b.conjunction();
      String v =
        "%" + q.toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
      return b.or(
        Arrays.stream(fields)
          .map(f -> b.like(b.lower(r.get(f)), v, '\\'))
          .toArray(jakarta.persistence.criteria.Predicate[]::new)
      );
    };
  }

  public static <T> Specification<T> eq(String field, Object value) {
    return (r, c, b) ->
      value == null || value.toString().isBlank()
        ? b.conjunction()
        : b.equal(r.get(field), value);
  }
}
