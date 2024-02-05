package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApproveDonationResponseDto {
    private String aid;
    private String tid;
    private String cid;
    private String sid;
    private String partner_order_id;
    private String partner_user_id;
    private String payment_method_type;
    private Amount amount;
    private CardInfo card_info;
    private String item_name;
    private String item_code;
    private String quantity;
    private String created_at;
    private String approved_at;
    private String payload;

    @Getter
    public static class Amount {
        private String total;
        private String tax_free;
        private String vat;
        private String point;
        private String discount;
    }

    @Getter
    public static class CardInfo {
        private String interest_free_install;
        private String bin;
        private String card_type;
        private String card_mid;
        private String approved_id;
        private String install_month;
        private String installment_type;
        private String kakaopay_purchase_corp;
        private String kakaopay_purchase_corp_code;
        private String kakaopay_issuer_corp_code;
    }
}