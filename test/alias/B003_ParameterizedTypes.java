import java.util.List;

class B003_ParameterizedTypes {
    {
        java.util.List < ? > list;
        java.util.List<String  >list2;
        java.util.Map   <Number,Double> map;
        java.util.Map<
        List<String>, List<List<Double>>> map2;
        java.util.List<?extends Number> extending;
        java.util.List< ?super Integer> withSuper;
        java.util.List< ? extends List<String> > list3;
    }
}