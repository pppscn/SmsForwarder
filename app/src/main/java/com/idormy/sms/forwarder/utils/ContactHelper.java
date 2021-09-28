package com.idormy.sms.forwarder.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.idormy.sms.forwarder.model.PhoneBookEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取联系人工具类
 */
@SuppressWarnings("unused")
public class ContactHelper {

    private static final String[] PROJECTION = new String[]{
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    private final List<PhoneBookEntity> contacts = new ArrayList<>();

    private ContactHelper() {

    }

    public static ContactHelper getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static class InstanceHolder {
        private static final ContactHelper INSTANCE = new ContactHelper();
    }

    /**
     * 获取所有联系人
     *
     * @param context 上下文
     * @return 联系人集合
     */
    public List<PhoneBookEntity> getContacts(Context context) {
        contacts.clear();
        ContentResolver cr = context.getContentResolver();
        try (Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, "sort_key")) {
            if (cursor != null) {
                final int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int mobileNoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String mobileNo, displayName;
                while (cursor.moveToNext()) {
                    mobileNo = cursor.getString(mobileNoIndex);
                    displayName = cursor.getString(displayNameIndex);
                    contacts.add(new PhoneBookEntity(displayName, mobileNo));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contacts;
    }

    /**
     * 通过姓名获取联系人
     *
     * @param context     上下文
     * @param contactName 联系人姓名
     * @return 联系人集合
     */
    public List<PhoneBookEntity> getContactByName(Context context, String contactName) {
        contacts.clear();

        ContentResolver cr = context.getContentResolver();
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like ? ";
        String[] selectionArgs = new String[]{"%" + contactName + "%"};
        try (Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, selection, selectionArgs, "sort_key")) {
            if (cursor != null) {
                final int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int mobileNoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String mobileNo, displayName;
                while (cursor.moveToNext()) {
                    mobileNo = cursor.getString(mobileNoIndex);
                    displayName = cursor.getString(displayNameIndex);
                    contacts.add(new PhoneBookEntity(displayName, mobileNo));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contacts;
    }

    /**
     * 通过手机号获取联系人
     *
     * @param context  上下文
     * @param phoneNum 手机号码
     * @return 联系人集合
     */
    public List<PhoneBookEntity> getContactByNumber(Context context, String phoneNum) {
        contacts.clear();
        Cursor cursor = null;
        if (phoneNum.length() < 11) {
            return null;
        }
        try {
            ContentResolver cr = context.getContentResolver();
            String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " in(?,?,?) ";
            String phone1 = phoneNum.subSequence(0, 3) + " " + phoneNum.substring(3, 7) +
                    " " + phoneNum.substring(7, 11);
            String phone2 = phoneNum.subSequence(0, 3) + "-" + phoneNum.substring(3, 7) +
                    "-" + phoneNum.substring(7, 11);

            String[] selectionArgs = new String[]{phoneNum, phone1, phone2};

            cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, selection, selectionArgs, "sort_key");
            if (cursor != null) {
                final int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int mobileNoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String mobileNo, displayName;
                while (cursor.moveToNext()) {
                    mobileNo = cursor.getString(mobileNoIndex);
                    displayName = cursor.getString(displayNameIndex);
                    contacts.add(new PhoneBookEntity(displayName, mobileNo));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contacts;
    }

    /**
     * 分页查询联系人
     *
     * @param context  上下文
     * @param page     页数
     * @param pageSize 每页数据量
     * @return 联系人集合
     */
    public List<PhoneBookEntity> getContactsByPage(Context context, int page, int pageSize) {
        contacts.clear();

        Cursor cursor = null;
        ContentResolver cr = context.getContentResolver();
        try {
            String sortOrder = "_id  limit " + page + "," + pageSize;
            cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, sortOrder);
            if (cursor != null) {
                final int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int mobileNoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String mobileNo, displayName;
                while (cursor.moveToNext()) {
                    mobileNo = cursor.getString(mobileNoIndex);
                    displayName = cursor.getString(displayNameIndex);
                    contacts.add(new PhoneBookEntity(displayName, mobileNo));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contacts;
    }

    /**
     * 获取联系人总数
     *
     * @param context 上下文
     * @return 数量
     */
    public int getContactCount(Context context) {
        ContentResolver cr = context.getContentResolver();
        try (Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, "sort_key")) {
            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}