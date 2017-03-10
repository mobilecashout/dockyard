# Dockyard

Centralized component discovery made easy.

[![](https://jitpack.io/v/mobilecashout/dockyard.svg)](https://jitpack.io/#mobilecashout/dockyard)

## About

Dockyard is a simple, compile-time component discovery service for Java applications that seamlessly integrates with 
dependency injection frameworks like Dagger and Guice. It is developed and tested with Dagger 2, used internally
by MobileCashout.

## Installation

Add the JitPack repository:

```xml
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
```

Add the dependency:

```xml
	<dependency>
	    <groupId>com.github.mobilecashout</groupId>
	    <artifactId>dockyard</artifactId>
	    <version>VERSION</version>
	</dependency>
```

## Usage

Consider having this interface:

```java
interface MyValueProvider {}
```

It is expected you will have one or more implementations of this interface. The specific use case of Dockyard is when you have more than one implementation and you require all of those implementations to do a certain task. 

It can be done by annotating these implementations with `@Dockyard` annotation - they will then
be picked up by Dockyard compiler and registered in a container.

```java
import com.mobilecashout.dockyard.Dockyard;

interface MyValueProvider {}

@Dockyard(MyValueProvider.class)
class SimpleValueProvider implements MyValueProvider{}

@Dockyard(MyValueProvider.class)
class ComplexValueProvider implements MyValueProvider{}
```

Having an implementation like this will have Dockyard pick up both of the classes and
generate a container class like this:

```java
import javax.inject.Inject;
//...

public class MyValueProviderDockyard implements DockyardContainer {
    protected final MyValueProvider[] instances;

    @Inject
    public MyValueProviderDockyard(

            final SimpleValueProvider a0,
            final ComplexValueProvider a1
    ) {
        this.instances = new MyValueProvider[]{a0, a1};
    }

    public MyValueProvider[] getAll() {
        return this.instances;
    }
}
```

If you look at the generated code, you can notice the `@Inject` annotation - 
Dockyard is designed only to locate and bind the components, not to instantiate them. 
Instantiation should be done in your dependency injection container.

In both Dagger and Guice this can be done by simply injecting the `*Dockyard` component as a
dependency.

Name of the generated class will be determined by the class you are binding the component to,
in this case, it is interface `MyValueProvider`, therefore the name of the component - 
`MyValueProviderDockyard`.

### Adding single instance to multiple dockyards

You can add single class to multiple containers like this:

```java
@Dockyard({MyValueProvider.class, MyOtherProvider.class})
```

Note, that you can only add class to container if the class does implement the interface or extends container class
somewhere along the chain.

### Injecting named instance

Dockyard will attach `@Named` annotation to the field being injected in to the container if you add a `name` key to Dockyard annotaiton like this.
This allows you to inject named instance of the given class if it is named in your dependency injection container giving slightly more
flexibility as to which instance should be used.

```java
@Dockyard(value = {MyValueProvider.class}, name = "some_name")
```

## License

Apache 2.0 License.