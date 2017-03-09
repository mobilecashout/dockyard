package com.mobilecashout.test;

import com.mobilecashout.dockyard.Dockyard;

interface InterfaceA {}
interface InterfaceB {}

@Dockyard(value = {InterfaceA.class, InterfaceB.class}, name = "hello")
class TestA implements InterfaceA, InterfaceB{
}

@Dockyard(value = {InterfaceA.class}, name = "world")
class TestB implements InterfaceA{
}