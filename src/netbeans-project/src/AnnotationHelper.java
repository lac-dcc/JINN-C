import java.lang.annotation.*;
//
// Annotation @AdaptiveMethod
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@interface AdaptiveMethod{}

//
// Annotation @Input
@Repeatable(Inputs.class)
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@interface Input{
	String param() default "";
}

//
// Array of annotations @Input
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@interface Inputs {
    Input[] value();
}

//
// Annotation @HiddenInput
@Repeatable(HiddenInputs.class)
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@interface HiddenInput{
	String expr() default "";
}

//
// Array of annotations @HiddenInput
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@interface HiddenInputs {
    HiddenInput[] value();
}

//
// Annotation @AdaptiveClass
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface AdaptiveClass{}


