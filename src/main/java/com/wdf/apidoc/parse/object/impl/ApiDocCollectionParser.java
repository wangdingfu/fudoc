package com.wdf.apidoc.parse.object.impl;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.wdf.apidoc.constant.enumtype.ApiDocObjectType;
import com.wdf.apidoc.constant.enumtype.CommonObjectType;
import com.wdf.apidoc.parse.ObjectParserExecutor;
import com.wdf.apidoc.parse.object.AbstractApiDocObjectParser;
import com.wdf.apidoc.pojo.bo.ParseObjectBO;
import com.wdf.apidoc.pojo.desc.ObjectInfoDesc;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author wangdingfu
 * @description 集合解析器
 * @Date 2022-04-18 21:33:19
 */
public class ApiDocCollectionParser extends AbstractApiDocObjectParser {


    @Override
    protected ApiDocObjectType getObjectType() {
        return ApiDocObjectType.COLLECTION_OBJECT;
    }


    /**
     * 执行顺序 在默认解析器前执行
     *
     * @return 返回值需要大于默认对象解析器 即要排在默认对象解析器前解析
     */
    @Override
    public int sort() {
        return 100;
    }

    /**
     * 判断是否为集合类型
     *
     * @param psiType 对象类型
     * @return true标识该类型为集合类型 可以解析该类型
     */
    @Override
    public boolean isParse(PsiType psiType) {
        return InheritanceUtil.isInheritor(psiType, CommonClassNames.JAVA_UTIL_COLLECTION);
    }

    /**
     * 解析集合类型
     *
     * @param psiType 对象类型
     * @return 集合类型解析后的描述对象
     */
    @Override
    public ObjectInfoDesc parse(PsiType psiType, ParseObjectBO parseObjectBO) {
        //获取集合的泛型对象
        PsiType genericsType = PsiUtil.extractIterableTypeParameter(psiType, false);
        if (Objects.isNull(genericsType)) {
            //没有泛型 无法继续遍历 直接退出解析
            return buildDefault(psiType, getCollectionType(null), parseObjectBO);
        }
        String canonicalText = genericsType.getCanonicalText();
        CommonObjectType commonObjectType = CommonObjectType.getEnum(canonicalText);
        String name = commonObjectType.getName();
        if (commonObjectType.isPrimitiveOrCommon()) {
            ApiDocObjectType apiDocObjectType = commonObjectType.getApiDocObjectType();
            //基本数据类型和公共数据类型 可以直接解析返回
            return buildDefault(psiType, getCollectionType(name), parseObjectBO, data -> data.setGenericsType(apiDocObjectType));
        }
        ObjectInfoDesc objectInfoDesc = buildDefault(psiType, getCollectionType(name), parseObjectBO);
        ParseObjectBO genericsParseObjectBO = new ParseObjectBO();
        genericsParseObjectBO.setApiDocContext(parseObjectBO.getApiDocContext());
        genericsParseObjectBO.setGenericsMap(buildGenericsMap(genericsType, PsiUtil.resolveClassInType(psiType)));
        ObjectInfoDesc genericsInfoDesc = ObjectParserExecutor.execute(genericsType, genericsParseObjectBO);
        if (Objects.nonNull(genericsInfoDesc)) {
            //将泛型对象的字段集合设置到当前apiDoc中
            objectInfoDesc.setChildList(genericsInfoDesc.getChildList());
            objectInfoDesc.setGenericsType(genericsInfoDesc.getApiDocObjectType());
        }
        return objectInfoDesc;
    }


    private String getCollectionType(String name) {
        if (StringUtils.isBlank(name)) {
            return "array";
        }
        return String.format("array[%s]", name);
    }


}
