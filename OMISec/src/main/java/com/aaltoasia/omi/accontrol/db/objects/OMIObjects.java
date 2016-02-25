package com.aaltoasia.omi.accontrol.db.objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Created by romanfilippov on 22/11/15.
 */
@XmlRootElement(name="Objects", namespace = "odf.xsd")
@XmlAccessorType(XmlAccessType.FIELD)
public class OMIObjects {

    @XmlElement(name="Object", namespace = "odf.xsd")
    private ArrayList<OMIObject> objects = new ArrayList<OMIObject>();

    public ArrayList<OMIObject> getObjects() {
        return objects;
    }

    public void setObjects(ArrayList<OMIObject> subObjects) {
        this.objects = subObjects;
    }

    public String xPath;

}
