package vn.marketplace.catalog.adapter.product.management.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.marketplace.catalog.application.product.ModerateProduct;
import vn.marketplace.catalog.application.product.ModerateProductCmd;
import vn.marketplace.catalog.application.product.ModerationAction;
import vn.marketplace.catalog.application.product.ProductModerationView;
import vn.marketplace.catalog.domain.shared.Actor;

/** Transactional boundary for the admin moderation use-case (approve / reject). */
@Service
public class ModerationFacade {

    private final ModerateProduct moderateProduct;

    public ModerationFacade(ModerateProduct moderateProduct) {
        this.moderateProduct = moderateProduct;
    }

    @Transactional
    public ProductModerationView approve(String productId, Actor moderator) {
        return moderateProduct.execute(
                new ModerateProductCmd(productId, moderator, ModerationAction.APPROVE, null));
    }

    @Transactional
    public ProductModerationView reject(String productId, String reason, Actor moderator) {
        return moderateProduct.execute(
                new ModerateProductCmd(productId, moderator, ModerationAction.REJECT, reason));
    }
}
