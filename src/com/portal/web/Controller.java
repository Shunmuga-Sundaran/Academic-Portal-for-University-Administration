package com.portal.web;

/** Every controller registers its own URLs on the router (polymorphism: the app treats them all alike). */
public interface Controller {
    void register(Router router);
}
