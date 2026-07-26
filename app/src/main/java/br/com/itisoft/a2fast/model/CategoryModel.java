package br.com.itisoft.a2fast.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategoryModel {
    public String UnicodeString;
    public String UnicodeIndex;
    public String Name;
    public boolean IsSelected;
    public String Guid = UUID.randomUUID().toString();

    public CategoryModel copy() {
        CategoryModel copy = new CategoryModel();
        copy.UnicodeString = UnicodeString;
        copy.UnicodeIndex = UnicodeIndex;
        copy.Name = Name;
        copy.Guid = Guid;
        return copy;
    }
}
