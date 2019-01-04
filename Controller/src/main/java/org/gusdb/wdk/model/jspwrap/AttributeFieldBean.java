/**
 * 
 */
package org.gusdb.wdk.model.jspwrap;

import java.util.Map;

import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.record.attribute.plugin.AttributePluginReference;
import org.gusdb.wdk.model.report.AttributeReporterRef;

public class AttributeFieldBean extends FieldBean {

    protected AttributeField attributeField;

    /**
     * 
     */
    public AttributeFieldBean(AttributeField field) {
        super(field);
        this.attributeField = field;
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.record.attribute.AttributeField#isSortable()
     */
    public boolean isSortable() {
        return attributeField.isSortable();
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.record.attribute.AttributeField#getAlign()
     */
    public String getAlign() {
        return attributeField.getAlign();
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.record.Field#getHelp()
     */
    @Override
    public String getHelp() {
        return attributeField.getHelp();
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.record.attribute.AttributeField#isRemovable()
     */
    public boolean isRemovable() {
        return attributeField.isRemovable();
    }

    public Map<String, AttributePluginReference> getAttributePlugins() {
        return attributeField.getAttributePlugins();
    }

    public Map<String, AttributeReporterRef> getAttributeReporters() {
        return attributeField.getReporters();
    }
}
