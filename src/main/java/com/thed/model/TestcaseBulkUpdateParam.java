package com.thed.model;

import java.util.List;
import java.util.Map;

public class TestcaseBulkUpdateParam {

    private String tag;
    private Map<String, Object> customFields;
    private List<TctTestcaseVersionParam> tctTestcaseVersionParam;
    private Integer tagsOperation;

    public boolean isFromJenkins() {
        return fromJenkins;
    }

    public void setFromJenkins(boolean fromJenkins) {
        this.fromJenkins = fromJenkins;
    }

    private boolean fromJenkins;


    public TestcaseBulkUpdateParam() {}

    public TestcaseBulkUpdateParam(String tag,
                                   Map<String, Object> customFields,
                                   List<TctTestcaseVersionParam> tctTestcaseVersionParam) {
        setTag(tag);
        setCustomFields(customFields);
        setTctTestcaseVersionParam(tctTestcaseVersionParam);
    }

    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public Integer getTagsOperation() {
        return tagsOperation;
    }

    public void setTagsOperation(Integer tagsOperation) {
        this.tagsOperation = tagsOperation;
    }

    public void setTctTestcaseVersionParam(List<TctTestcaseVersionParam> tctTestcaseVersionParam) {
        this.tctTestcaseVersionParam = tctTestcaseVersionParam;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public List<TctTestcaseVersionParam> getTctTestcaseVersionParam() {
        return tctTestcaseVersionParam;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
