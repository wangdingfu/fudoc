package com.wdf.fudoc.apidoc.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.wdf.fudoc.common.AbstractClassAction;
import com.wdf.fudoc.common.FuDocMessageBundle;
import com.wdf.fudoc.common.notification.FuDocNotification;
import com.wdf.fudoc.apidoc.config.state.FuDocSetting;
import com.wdf.fudoc.common.constant.MessageConstants;
import com.wdf.fudoc.apidoc.constant.enumtype.JavaClassType;
import com.wdf.fudoc.apidoc.data.FuDocData;
import com.wdf.fudoc.apidoc.data.FuDocDataContent;
import com.wdf.fudoc.apidoc.factory.FuDocServiceFactory;
import com.wdf.fudoc.apidoc.pojo.context.FuDocContext;
import com.wdf.fudoc.apidoc.service.FuDocService;
import com.wdf.fudoc.util.ClipboardUtil;
import com.wdf.fudoc.util.PsiClassUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author wangdingfu
 * @descption: 一键生成API接口文档入口类
 * @date 2022-04-16 18:53:05
 */
@Slf4j
public class GenFuDocAction extends AbstractClassAction {

    @Override
    protected boolean isShow(JavaClassType javaClassType) {
        return !JavaClassType.ANNOTATION.equals(javaClassType);
    }

    @Override
    protected String exceptionMsg() {
        return MessageConstants.NOTIFY_GEN_FAIL;
    }

    /**
     * 点击按钮或按下快捷键触发生成API接口文档方法
     *
     * @param e 点击事件
     */
    @Override
    protected void execute(AnActionEvent e, PsiClass psiClass, FuDocContext fuDocContext) {
        FuDocService fuDocService = FuDocServiceFactory.getFuDocService(JavaClassType.get(psiClass));
        if (Objects.nonNull(fuDocService)) {
            long start = System.currentTimeMillis();
            String content = fuDocService.genFuDocContent(fuDocContext, psiClass);
            if (StringUtils.isBlank(content)) {
                //通知没有可以生成接口文档的内容
                FuDocNotification.notifyWarn(FuDocMessageBundle.message(MessageConstants.NOTIFY_GEN_NO_CONTENT, psiClass.getName()));
                return;
            }
            //将接口文档内容拷贝至剪贴板
            ClipboardUtil.copyToClipboard(content);
            //通知接口文档已经拷贝至剪贴板
            FuDocNotification.notifyInfo(FuDocMessageBundle.message(MessageConstants.NOTIFY_COPY_OK, psiClass.getName()));
            log.info("生成接口文档【{}】完成. 共计耗时{}ms", psiClass.getName(), System.currentTimeMillis() - start);
        }
    }

}
