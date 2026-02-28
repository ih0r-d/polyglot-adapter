# Polyglot Adapter – Python Codegen Architecture

You are assisting in implementing a production-grade Python contract code generator
for a Java polyglot adapter (GraalPy integration).

The goal is NOT to execute Python.
The goal is to statically extract contracts from guest scripts and generate correct,
type-safe Java interfaces at build-time.

Execution of Python code is strictly forbidden.

---

# ENVIRONMENT

Java version: 25  
Java must be resolved from `.sdkmanrc` via:

    sdk env

Modern Java 25 features are allowed:
- records
- sealed interfaces
- pattern matching for switch
- text blocks
- modern collections APIs

Generated code must compile cleanly on Java 25.

---

# CORE ARCHITECTURAL PRINCIPLES

1. Build-time code generation only.
2. Deterministic public API extraction.
3. Type correctness over aggressive inference.
4. Never generate incorrect Java types.
5. Backend abstraction must allow parser replacement.
6. Preserve long-term extensibility.

---

# BACKEND ABSTRACTION (MANDATORY)

Define abstraction:

    interface PythonParserBackend {
        ContractModel parse(String source, CodegenConfig config);
    }

Current implementation:
PythonHeuristicBackend

Future implementations:
PythonAstBackend
PythonAntlrBackend

Core codegen pipeline must NOT depend on concrete backend.

Renderer and ContractModel must remain backend-agnostic.

---

# ROADMAP

## PHASE 1 – Stable Heuristic Backend (NOW)

✔ Backend abstraction
✔ PythonHeuristicBackend implementation
✔ Deterministic export handling
✔ Annotation-first typing
✔ Safe literal inference
✔ Safe fallback policy
✔ Configurable typing strategy
✔ Unit test coverage
✔ Java 25 clean compilation

No external parsing libraries in this phase.

---

## PHASE 2 – Advanced Python Backend

Optional advanced backend:

Option A:
PythonAstBackend (using Python ast.parse at build-time)

Option B:
PythonAntlrBackend (pure Java)

Backend must be swappable without changing:
- ContractModel
- JavaInterfaceGenerator
- CLI

---

## PHASE 3 – JavaScript Backend (Later)

After Python is stable:

- JS backend implementation
- Reuse PolyType model
- Same backend abstraction approach

Do not implement JS in Phase 1.

---

# SUPPORTED ENTRYPOINTS (MANDATORY)

Only explicitly exported symbols define contract.

Supported:

1. Exported class:

   polyglot.export_value("ApiName", ApiClass)

2. Exported namespace dictionary:

   polyglot.export_value("ApiName", {
   "method1": method1,
   "method2": method2
   })

3. (Optional future)
   @polyglot.export_value decorator

Not supported:
- globals()
- implicit public detection
- reflection-based discovery

---

# CONTRACT RULES

- Only exported members appear in interface.
- Methods starting with "_" are ignored.
- __init__ must never appear in interface.
- Deterministic method ordering.
- Generated Java must compile cleanly.

---

# TYPING STRATEGY

Priority order:

1. Explicit Python annotations (first-class support)
2. Safe literal inference
3. Safe wrapper inference
4. Fallback (never guess)

---

# SUPPORTED SAFE INFERENCE (PHASE 1)

List literal:

    [1,2,3]            -> List<Integer>
    ["a","b"]          -> List<String>
    [1,"2"]            -> List<Object>

Dict literal:

    {"a":1}            -> Map<String,Integer>
    {"a":1,"b":"x"}    -> Map<String,Object>

Primitives:

    5                  -> Integer
    5.2                -> Double
    "abc"              -> String
    true/false         -> Boolean

Wrappers:

    list([1,2])
    dict({"a":1})
    dict(a=1,b=2)

---

# NEVER INFER FROM

- Function calls
- Variables
- Comprehensions
- External libraries
- Dynamic runtime values

Those must fallback safely.

---

# FALLBACK POLICY

Unknown scalar:
-> Object

Unknown list:
-> List<Object>

Unknown dict:
-> Map<String,Object>

Never collapse collections to Object.
Always preserve structure.

Warnings must be emitted when fallback occurs.

---

# CONFIGURATION

CodegenConfig must support:

- typingStrategy = BALANCED | STRICT
- failOnAmbiguous = boolean
- warnOnFallback = boolean

BALANCED:
allow fallback, emit warnings

STRICT:
ambiguous types cause failure

---

# POLYTYPE MODEL (JAVA 25)

Use sealed hierarchy:

    sealed interface PolyType permits PolyPrimitive, PolyList, PolyMap, PolyUnknown

PolyPrimitive:
INT
FLOAT
STRING
BOOLEAN

PolyList:
elementType: PolyType

PolyMap:
keyType: PolyType
valueType: PolyType

PolyUnknown:
represents unknown type

Pattern matching for switch must be used.

---

# CRITICAL DESIGN RULE

Correctness > aggressiveness.

When unsure:
degrade safely.

Never produce incorrect Java type.

---

# DEFINITION OF DONE – PYTHON PHASE

✔ Backend abstraction implemented
✔ Heuristic backend stable
✔ Export class supported
✔ Export dict namespace supported
✔ Annotation-first typing works
✔ Literal inference works
✔ Mixed types degrade safely
✔ Fallback preserves collection structure
✔ Config works
✔ Full unit test coverage
✔ Java 25 compilation clean
✔ No generated code warnings

---

# LONG-TERM VISION

This codegen must be:

- deterministic
- backend-swappable
- extensible to JS
- suitable for OSS use
- safe for enterprise CI

The architecture must prevent tight coupling to any single parsing strategy.