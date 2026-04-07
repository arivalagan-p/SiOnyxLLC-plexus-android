package com.sionyx.plexus.ui.dialog;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sionyx.plexus.utils.Event;

public class TimeZoneViewModel extends AndroidViewModel {
    public TimeZoneViewModel(@NonNull Application application) {
        super(application);
    }

    private final MutableLiveData<Event<Boolean>> _isSelectTimeZoneCancelIcon = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectTimeZoneCancelIcon = _isSelectTimeZoneCancelIcon;

    private final MutableLiveData<Event<Integer>> _isSelectTimeZone = new MutableLiveData<>();
    public LiveData<Event<Integer>> isSelectTimeZone = _isSelectTimeZone;

    public void onCancelTimeZoneView() {
        _isSelectTimeZoneCancelIcon.setValue(new Event<>(true));
    }

    public void hasSelectTimeZone(int selectedPosition) {
        _isSelectTimeZone.setValue(new Event<>(selectedPosition));
    }

    public String[] settingsArrayList = {
            "(GMT-01:00) Azores",
            "(GMT-01:00) Cape Verde Islands",
            "(GMT-02:00) Mid-Atlantic",
            "(GMT-03:00) Brasilia",
            "(GMT-03:00) Buenos Aires, Georgetown",
            "(GMT-03:00) Greenland",
            "(GMT-03:30) Newfoundland and Labrador",
            "(GMT-04:00) Atlantic Time (Canada)",
            "(GMT-04:00) Caracas, La Paz",
            "(GMT-04:00) Manaus",
            "(GMT-04:00) Santiago",
            "(GMT-05:00) Bogota, Lima, Quito",
            "(GMT-05:00) Eastern Time (US and Canada)",
            "(GMT-05:00) Indiana (East)",
            "(GMT-06:00) Central America",
            "(GMT-06:00) Central Time (US and Canada)",
            "(GMT-06:00) Guadalajara, Mexico City, Monterrey",
            "(GMT-06:00) Saskatchewan",
            "(GMT-07:00) Arizona",
            "(GMT-07:00) Chihuahua, La Paz, Mazatlan",
            "(GMT-07:00) Mountain Time (US and Canada)",
            "(GMT-08:00) Pacific Time (US and Canada); Tijuana",
            "(GMT-09:00) Alaska",
            "(GMT-10:00) Hawaii",
            "(GMT-11:00) Midway Island, Samoa",
            "(GMT+00:00) Casablanca, Monrovia",
            "(GMT+00:00) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London",
            "(GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna",
            "(GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague",
            "(GMT+01:00) Brussels, Copenhagen, Madrid, Paris",
            "(GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb",
            "(GMT+01:00) West Central Africa",
            "(GMT+02:00) Athens, Bucharest, Istanbul",
            "(GMT+02:00) Cairo",
            "(GMT+02:00) Harare, Pretoria",
            "(GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius",
            "(GMT+02:00) Jerusalem",
            "(GMT+02:00) Minsk",
            "(GMT+02:00) Windhoek",
            "(GMT+03:00) Baghdad",
            "(GMT+03:00) Kuwait, Riyadh",
            "(GMT+03:00) Moscow, St. Petersburg, Volgograd",
            "(GMT+03:00) Nairobi",
            "(GMT+03:30) Tehran",
            "(GMT+04:00) Abu Dhabi, Muscat",
            "(GMT+04:00) Baku",
            "(GMT+04:00) Tblisi",
            "(GMT+04:00) Yerevan",
            "(GMT+04:30) Kabul",
            "(GMT+05:00) Ekaterinburg",
            "(GMT+05:00) Islamabad, Karachi, Tashkent",
            "(GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi",
            "(GMT+05:45) Kathmandu",
            "(GMT+06:00) Almaty, Novosibirsk",
            "(GMT+06:00) Astana, Dhaka",
            "(GMT+06:00) Sri Jayawardenepura",
            "(GMT+06:30) Yangon (Rangoon)",
            "(GMT+07:00) Bangkok, Hanoi, Jakarta",
            "(GMT+07:00) Krasnoyarsk",
            "(GMT+08:00) Beijing, Chongqing, Hong Kong SAR, Urumqi",
            "(GMT+08:00) Irkutsk, Ulaanbaatar",
            "(GMT+08:00) Kuala Lumpur, Singapore",
            "(GMT+08:00) Perth",
            "(GMT+08:00) Taipei",
            "(GMT+09:00) Osaka, Sapporo, Tokyo",
            "(GMT+09:00) Seoul",
            "(GMT+09:00) Yakutsk",
            "(GMT+09:30) Adelaide",
            "(GMT+09:30) Darwin",
            "(GMT+10:00) Brisbane",
            "(GMT+10:00) Canberra, Melbourne, Sydney",
            "(GMT+10:00) Guam, Port Moresby",
            "(GMT+10:00) Hobart",
            "(GMT+10:00) Vladivostok",
            "(GMT+11:00) Magadan, Solomon Islands, New Caledonia",
            "(GMT+12:00) Auckland, Wellington",
            "(GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands",
            "(GMT+13:00) Nuku'alofa"};
}