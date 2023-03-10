package demiglace.javaparser;

import com.github.javaparser.utils.SourceRoot;

import java.util.HashMap;
import java.util.List;

public class JavaParsingResult {
    private List<SourceRoot> sourceRoots;
    private HashMap<String, List<ResolvedMethodCall>> methodMap;

    public List<SourceRoot> getSourceRoots() {
        return sourceRoots;
    }

    public void setSourceRoots(List<SourceRoot> sourceRoots) {
        this.sourceRoots = sourceRoots;
    }

    public HashMap<String, List<ResolvedMethodCall>> getMethodMap() {
        return methodMap;
    }

    public void setMethodMap(HashMap<String, List<ResolvedMethodCall>> methodMap) {
        this.methodMap = methodMap;
    }

    public void addToMethodMap(HashMap<String, List<ResolvedMethodCall>> addedMethods) {
        if (methodMap == null) {
            methodMap = new HashMap<>();
        }
        methodMap.putAll(addedMethods);
    }
}
