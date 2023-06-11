package com.wdf.fudoc.request.view;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.wdf.fudoc.common.FuTab;
import com.wdf.fudoc.components.factory.FuTabBuilder;
import com.wdf.fudoc.request.po.FuRequestConfigPO;
import com.wdf.fudoc.request.po.GlobalPreScriptPO;
import com.wdf.fudoc.request.tab.settings.GlobalConfigTab;
import com.wdf.fudoc.request.tab.settings.GlobalHeaderTab;
import com.wdf.fudoc.request.tab.settings.GlobalPreScriptTab;
import com.wdf.fudoc.request.tab.settings.GlobalVariableTab;
import com.wdf.fudoc.storage.FuRequestConfigStorage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【Fu Request】设置面板
 * 1、全局变量维护
 * 2、全局配置维护
 * 3、公共请求头维护
 *
 * @author wangdingfu
 * @date 2022-12-07 21:08:51
 */
public class FuRequestSettingView extends DialogWrapper {

    /**
     * 根面板
     */
    @Getter
    private final JPanel rootPanel;

    /**
     * tab页构建器
     */
    private final FuTabBuilder fuTabBuilder = FuTabBuilder.getInstance();

    private final Project project;

    private GlobalConfigTab globalConfigTab;
    private GlobalVariableTab globalVariableTab;
    private GlobalHeaderTab globalHeaderTab;

    private final FuRequestConfigPO configPO;

    private final FuRequestConfigStorage storage;

    private final AtomicInteger preScriptIndex = new AtomicInteger(0);

    private final List<GlobalPreScriptTab> preScriptTabs = Lists.newArrayList();


    public FuRequestSettingView(@Nullable Project project) {
        super(project, true);
        this.project = project;
        this.rootPanel = new JPanel(new BorderLayout());
        this.storage = FuRequestConfigStorage.getInstance(project);
        this.configPO = this.storage.readData();
        setTitle("【Fu Request】设置");
        initPanel();
        init();
    }


    /**
     * 初始化设置面板
     */
    private void initPanel() {
        this.globalConfigTab = new GlobalConfigTab();
        this.globalVariableTab = new GlobalVariableTab();
        this.globalHeaderTab = new GlobalHeaderTab();
        //初始化数据
        initData();
        //添加tab页
        fuTabBuilder
                //全局请求头
                .addTab(this.globalHeaderTab)
                //请求配置
                .addTab(this.globalConfigTab)
                //全局变量
                .addTab(this.globalVariableTab);
        //添加前置脚本tab
        this.preScriptTabs.forEach(fuTabBuilder::addTab);
        this.rootPanel.add(fuTabBuilder.build(), BorderLayout.CENTER);
    }


    /**
     * 初始化数据
     */
    public void initData() {
        //初始化全局请求头
        this.globalHeaderTab.initData(this.configPO);
        //初始化全局变量
        this.globalVariableTab.initData(this.configPO);
        //初始化全局前置脚本
        Map<String, GlobalPreScriptPO> preScriptMap = this.configPO.getPreScriptMap();
        //如果不存在默认前置脚本数据 则需要添加上默认的前置脚本数据
        GlobalPreScriptPO globalPreScriptPO = preScriptMap.get(GlobalPreScriptTab.TITLE);
        if (Objects.isNull(globalPreScriptPO)) {
            globalPreScriptPO = new GlobalPreScriptPO();
            preScriptMap.put(GlobalPreScriptTab.TITLE, globalPreScriptPO);
        }
        //对前置脚本排序(为了展示顺序性) 创建前置脚本tab 并 初始化数据
        Lists.newArrayList(preScriptMap.keySet()).stream().sorted().forEach(f -> this.preScriptTabs.add(new GlobalPreScriptTab(project, f, configPO)));

    }


    /**
     * 点击ok按钮时触发保存
     */
    @Override
    protected void doOKAction() {
        this.apply();
        super.doOKAction();
    }

    public void apply() {
        //持久化配置数据
        this.preScriptTabs.forEach(f -> f.saveData(configPO));
        this.globalHeaderTab.saveData(configPO);
        this.globalVariableTab.saveData(configPO);
        this.storage.saveData(configPO);
    }

    @Override
    protected Action @NotNull [] createActions() {
        List<Action> actionList = Lists.newArrayList(super.createActions());
        actionList.add(new CreateTabAction("新增前置脚本"));
        actionList.add(new RemoveTabAction("删除前置脚本"));
        return actionList.toArray(new Action[]{});
    }


    protected class CreateTabAction extends DialogWrapperAction {
        protected CreateTabAction(String title) {
            super(title);
            putValue(Action.SMALL_ICON, AllIcons.General.Add);
        }

        @Override
        protected void doAction(ActionEvent e) {
            //新增前置脚本
            GlobalPreScriptTab globalPreScriptTab = new GlobalPreScriptTab(project, GlobalPreScriptTab.TITLE + preScriptIndex.incrementAndGet(), null);
            preScriptTabs.add(globalPreScriptTab);
            fuTabBuilder.addTab(globalPreScriptTab);
        }
    }

    protected class RemoveTabAction extends DialogWrapperAction {
        protected RemoveTabAction(String title) {
            super(title);
            putValue(Action.SMALL_ICON, AllIcons.General.Remove);
        }

        @Override
        protected void doAction(ActionEvent e) {
            //新增前置脚本
            FuTab selected = fuTabBuilder.getSelected();
            if (Objects.nonNull(selected)) {
                String text = selected.getTabInfo().getText();
                if (GlobalPreScriptTab.TITLE.equals(text)) {
                    return;
                }
                boolean isDelete = preScriptTabs.removeIf(f -> f.getTabInfo().getText().equals(text));
                if (isDelete) {
                    fuTabBuilder.select(GlobalPreScriptTab.TITLE);
                    fuTabBuilder.removeTab(text);
                    fuTabBuilder.revalidate();
                    Map<String, GlobalPreScriptPO> preScriptMap = configPO.getPreScriptMap();
                    preScriptMap.remove(text);
                }
            }
        }
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.rootPanel;
    }
}
