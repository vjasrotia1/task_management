package com.varun.taskmgmtapi.models;

public class ClientModel {
    public static void main(String[] args) {
//            Product p=new Product("iphone");
//            p.setTitle("macbook");
//            System.out.println(p.getTitle());

        User u=new User();
        u.setRole(Role.ADMIN);
        Role r1= u.getRole();
        Role r2=Role.USER;
        System.out.println(r1.equals(r2));

    }
}
