package name.xunicorn.iocipherbrowserext.components.activities.main;


import name.xunicorn.iocipherbrowserext.activities.MainActivity;

public class MenuComponent {

    private static MenuComponent instance;

    final MainActivity activity;

    private MenuComponent(MainActivity activity) {
        this.activity = activity;
    }

    public static MenuComponent getComponent(MainActivity activity) {
        if(instance == null) {
            instance = new MenuComponent(activity);
        }

        return instance;
    }
}
