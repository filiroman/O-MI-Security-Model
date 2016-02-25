package com.aaltoasia.omi.accontrol.db.objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Created by romanfilippov on 22/11/15.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class OMIInfoItem {

    @XmlAttribute
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String xPath;
}
