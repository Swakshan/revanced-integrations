package app.revanced.integrations.twitter.patches.customise;

import android.util.*;
import java.util.*;
import java.lang.reflect.Field;
import app.revanced.integrations.twitter.Pref;

public class NavBar {

    public static void logger(Object j){
        Log.d("piko", j.toString());
    }


    public static List navBar(List inp){
        try{
                ArrayList choices = Pref.customNavbar();
                List list2 = new ArrayList<>(inp);
                Iterator itr = list2.iterator();

                while (itr.hasNext()) {
                    Object obj = itr.next();
                    String itemStr = obj.toString();
                    if(choices.contains(itemStr)){
                        inp.remove(obj);
                    }
                }

        }catch (Exception e){
            logger(e);
        }
        return inp;
    }
}