package com.giftforyoube.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Embeddable
@NoArgsConstructor
public class RelatedUrl {
    private static final int MAX_LENGTH = 255;

    @Column(nullable = false, length = MAX_LENGTH)
    private String url;

    public RelatedUrl(String url) {
//        if (isNotValidRelatedURL(url)) {
//            throw new CustomException(ErrorCode.NOT_VALID_URL);
//        }
        this.url = url;
    }

    private boolean isNotValidRelatedURL(String url) {
        return Objects.isNull(url) || url.length() > MAX_LENGTH || url.isEmpty();
    }

    @Override
    public String toString() {
        return url;
    }
}
