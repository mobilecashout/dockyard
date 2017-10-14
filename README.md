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

Consider having an interface:

```java
interface MyValueProvider {}
```

It is common you will have one or more implementations of mentioned interface for many different reasons.
The specific use case of Dockyard is when  you have more than one implementation and you require all of those 
implementations to do a certain task, but do not want to manually inject all of them, and find it would be
easier to just have a list of those implementations you can then access later for whatever reason.

This can be achieved using Dockyard, namely by annotating these implementations with `@Dockyard` annotation - 
they will then be picked up by Dockyard annotation processor and registered in a generated container.

Consider this example:

```java
import com.mobilecashout.dockyard.Dockyard;

interface MyValueProvider {}

@Dockyard(MyValueProvider.class)
class SimpleValueProvider implements MyValueProvider{}

@Dockyard(MyValueProvider.class)
class ComplexValueProvider implements MyValueProvider{}
```

Given that you have two classes like this in your codebase, Dockyard would pick them up and generate a source file
containing a special container class:

```java
import javax.inject.Inject;
//...

import com.mobilecashout.dockyard.DockyardContainer;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MyValueProviderDockyard implements DockyardContainer {
    @Inject
    protected SimpleValueProvider a0;

    @Inject
    protected ComplexValueProvider a1;

    protected List<MyValueProvider> instances = null;

    @Inject
    public MyValueProviderDockyard() {
    }

    public List<MyValueProvider> getAll() {
        if (null == instances) {
            instances = Collections.unmodifiableList(Arrays.asList(new MyValueProvider[] {a0,a1}));
        }
        return instances;
    }
}
```

If you look at the generated code, you can notice the `@Inject` annotation - 
Dockyard is designed only to locate and compose the components, not to instantiate them. 
Instantiation should be done in your dependency injection container.

In both Dagger and Guice this can be done by simply injecting the `*Dockyard` component as a
dependency.

Name of the generated class will be determined by the class you are binding the component to,
in this case, it is interface `MyValueProvider`, therefore the name of the container - 
`MyValueProviderDockyard`.

### Limitations

You can not mark more than 65,534 classes for the same Dockyard container as this is the limit of fields
a Java class can have. In the unlikely scenario this is necessary, for example, in case of generated classes
etc, you will need to split them in multiple Dockyard containers and unify them back in code.

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

### Version history

#### 2.0.1
Oct 14, 2017

- Design fix: make instances variable protected and make the list of items be immutable.
- Design fix: Dockyard container itself is now marked as `Singleton`.

#### 2.0.0
Oct 14, 2017

- Now using fields instead of constructor parameters to inject container items, lifting the 255 item limit to 65,533.
- **BC breaking**: `DockyardContainer.getAll()` now returns `List<T>` instead of `T[]` Array.

#### 1.0.0
Mar 9, 2017

- Initial release

## License

Apache 2.0 License.
