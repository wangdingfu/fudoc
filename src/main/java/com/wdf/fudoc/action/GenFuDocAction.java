package com.wdf.fudoc.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.wdf.fudoc.FuDocMessageBundle;
import com.wdf.fudoc.FuDocNotification;
import com.wdf.fudoc.constant.MessageConstants;
import com.wdf.fudoc.constant.enumtype.JavaClassType;
import com.wdf.fudoc.data.FuDocData;
import com.wdf.fudoc.data.FuDocDataContent;
import com.wdf.fudoc.factory.FuDocServiceFactory;
import com.wdf.fudoc.pojo.context.FuDocContext;
import com.wdf.fudoc.service.FuDocService;
import com.wdf.fudoc.util.ClipboardUtil;
import com.wdf.fudoc.util.PsiClassUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author wangdingfu
 * @descption: 一键生成API接口文档入口类
 * @date 2022-04-16 18:53:05
 */
@Slf4j
public class GenFuDocAction extends AnAction {


    /**
     * 在点击右键显示操作栏时 会调用该方法判断是否显示生成接口按钮
     *
     * @param e 当前点击事件
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        PsiElement targetElement = PsiClassUtils.getTargetElement(e);
        PsiClass psiClass = PsiClassUtils.getPsiClass(targetElement);
        JavaClassType javaClassType = JavaClassType.get(psiClass);
        if (JavaClassType.isNone(javaClassType)) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        //目前FuDoc只支持Controller生成接口文档
        presentation.setEnabledAndVisible(true);
    }

    /**
     * 点击按钮或按下快捷键触发生成API接口文档方法
     *
     * @param e 点击事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        long start = System.currentTimeMillis();
        PsiElement targetElement = PsiClassUtils.getTargetElement(e);
        if (Objects.isNull(targetElement)) {
            FuDocNotification.notifyWarn(FuDocMessageBundle.message(MessageConstants.NOTIFY_NOT_FUND_CLASS));
            return;
        }
        //获取当前操作的类
        PsiClass psiClass = PsiClassUtils.getPsiClass(targetElement);
        //向全局上下文中添加Project内容
        FuDocDataContent.setData(FuDocData.builder().event(e).build());

        try {
            FuDocContext fuDocContext = new FuDocContext();
            fuDocContext.setTargetElement(targetElement);

            FuDocService fuDocService = FuDocServiceFactory.getFuDocService(JavaClassType.get(psiClass));
            if (Objects.nonNull(fuDocService)) {
                String content = fuDocService.genFuDocContent(fuDocContext, psiClass);
                //将接口文档内容拷贝至剪贴板
                ClipboardUtil.copyToClipboard(content);
                log.info("生成接口文档【{}】完成. 共计耗时{}ms", psiClass.getName(), System.currentTimeMillis() - start);
                //通知接口文档已经拷贝至剪贴板
                FuDocNotification.notifyInfo(FuDocMessageBundle.message(MessageConstants.NOTIFY_COPY_OK, psiClass.getName()));
            }
        } catch (Throwable exception) {
            //发送失败通知
            log.error("【Fu Doc】解析生成接口文档失败", exception);
            FuDocNotification.notifyError(FuDocMessageBundle.message(MessageConstants.NOTIFY_GEN_FAIL));
        } finally {
            FuDocDataContent.remove();
        }

    }
}
