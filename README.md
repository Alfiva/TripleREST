# TripleREST
TripleREST is a triple-based RESTful interface for Linked Data (think RDF). TripleREST consists on representing the triples that describe a resource in RDF as the path of a URL, with the RDF terms of subject, predicate and object as segments of the path, and manipulating these in a RESTful manner. Let’s assume `http://example.org` hosts an implementation of TripleREST backed with an RDF model or store, and that SUB, PRE and OBJ are the subject, predicate and object, respectively, of an RDF triple. Then, for example, these are some of the API calls you could make:

|      | URL                        | Body         | Result  | 
| ---- | ---------------------------| ------------ | ------  |
| GET  | `http://example.org/SUB`     |              | Returns a description of the SUB resource |
| GET  | `http://example.org/*/*/OBJ` |              | Returns all triples with OBJ as an object |
| POST | `http://example.org/SUB/PRE` | OBJ triples  | Adds OBJ resource in the model, and as the object of SUB-PRE-OBJ triple |

You can find the full definition of the TripleREST API in the Wiki, along with more examples and useful equivalences to SPARQL. The URIs of SUB, OBJ and PRE can't be written as-is in a URL. Characters can be escaped, or the prefixes can be extracted. For instance:

```
http://example.org/pref:subject?pref=http://mynamespace.org/

http://example.org/*/*/pref:object?pref=http://mynamespace.org/

http://example.org/pre1:subject/pre2:predicate?pre1=http://mynamespace1.org/&pre2=http://mynamespace2.org/
```

# Modules & Artifacts
This repostory contains 5 source projects:
* **triplerest.api**: This is the interface definition of TripleREST as a Java interface, without implementation.
* **triplerest.impl.rdf4j**: Implementation of the TripleREST interface over a RDF4J triple store.
* **triplerest.impl.uaal**: Implementation of the TripleREST interface over the universAAL IoT platform.
* **triplerest.server.servlet**: Exposes the TripleREST interface using an HTTP Java servlet.
* **triplerest.server.spark**: Exposes the TripleREST interface using the Spark web framework.

The whole structure is simple: You have 2 choices for how to expose the REST interface (servlet or Spark). These use the core TripleREST interface definition to access one of the two choices for the underlying implementation (RDF4J or universAAL).

# Build & Run
All projects are Maven artifacts and thus can be built with the `mvn install` command on their root folder, where the pom.xml file is.

The core **triplerest.api** project produces a simple .jar library, and can be used however you want in your own implementation. The implementation projects in this repository produce OSGi bundles when built. To run them, first choose only one option at each level (servlet or Spark for server, RDF4J or universAAL for backend), then install their bundles into your OSGi container (e.g. Karaf). Notice that you need to have the dependencies for each one before installing them:

* **triplerest.server.servlet**: Javax Servlet.
* **triplerest.server.spark**: Spark core.
* **triplerest.impl.rdf4j**: RDF4J libraries.
* **triplerest.impl.uaal**: unviersAAL platform with CHe and utilites API.

...And of course the **triplerest.api** itself. Since it is not an OSGi bundle but a simple .jar library, you will need the `wrap:` command when installing in OSGi.

# License and related work
All projects in the repository are licensed under the Apache Software License 2.0.

This software was created in the Universitat Politecnica de Valencia, ITACA institute, SABIEN group.

This work was possible thanks to:
* [The universAAL project](https://cordis.europa.eu/project/id/247950)
* [The ReAAL project](https://cordis.europa.eu/project/id/325189)
* [The universAAL IoT platform](https://www.universaal.info/)
