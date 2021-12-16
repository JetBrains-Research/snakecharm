Feature: Inspection: SmkUnresolvedReferenceInspectionExtension

  Scenario Outline: Quick fix fot missed files
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <section>: "<path>"
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection error on <<path>> with message
     """
     Unresolved reference '<path>'
     """
    When I check highlighting warnings
    And I invoke quick fix Create '<path>' and see text:
     """
     <section>: "<path>"
     """
    Examples:
      | path              | section                |
      | NAME.yaml         | rule NAME: conda       |
      | envs/NAME.yaml    | rule NAME: conda       |
      | ../envs/NAME.yaml | rule NAME: conda       |
      | NAME.py.ipynb     | rule NAME: notebook    |
      | NAME.py           | rule NAME: script      |
      | boo.smk           | module NAME: snakefile |
      | NAME.yaml         | configfile             |
      | NAME.yaml         | pepfile                |
      | NAME.yml          | pepschema              |

    # Impossible to check whether the file has been created because:
    # 1) It is being creating asynchronously
    # 2) So, we may need async refresh() (see LightTempDirTestFixtureImpl.java:137)
    # It leads to Exception: "Do not perform a synchronous refresh under read lock ..."