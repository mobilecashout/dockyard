package com.mobilecashout.test;

import com.mobilecashout.dockyard.Dockyard;

import javax.annotation.Generated;

interface InterfaceA {}
interface InterfaceB {}

@Dockyard(value = {InterfaceA.class, InterfaceB.class}, name = "hello")
class TestA implements InterfaceA, InterfaceB{
}

@Dockyard(value = {InterfaceA.class}, name = "world")
@Generated("com.mobilecashout.test.InterfaceA")
class TestB implements InterfaceA{
}
