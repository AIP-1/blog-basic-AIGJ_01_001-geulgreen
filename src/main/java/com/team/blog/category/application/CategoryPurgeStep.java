package com.team.blog.category.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 카테고리 삭제(글 정리 10 다음). */
@Component
public class CategoryPurgeStep implements WithdrawalPurgeStep {

    private final CategoryService categories;

    public CategoryPurgeStep(CategoryService categories) {
        this.categories = categories;
    }

    @Override
    public int order() {
        return 17;
    }

    @Override
    public void purge(long memberId) {
        categories.purgeWithdrawn(memberId);
    }
}
