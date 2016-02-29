package com.aaltoasia.omi.accontrol.db.objects;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by romanfilippov on 24/11/15.
 */
public class OMIGroup {

    public int id;
    public String name;

    @SerializedName("values")
    public ArrayList<Integer> userIDs;
}
