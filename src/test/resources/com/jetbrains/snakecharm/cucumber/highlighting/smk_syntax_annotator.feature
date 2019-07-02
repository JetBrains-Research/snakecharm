Feature: Annotate additional syntax
  This is not for syntax errors highlighting

  Scenario Outline: Annotate Rules
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        <section>: <text>
    """
    Then I expect inspection info on <NAME> with message
    """
    PY.FUNC_DEFINITION
    """
    Then I expect inspection info on <<section>> with message
    """
    <highlighting>
    """
    When I check highlighting infos
    Examples:
      | section              | text       | highlighting             |
      | output               | "file.txt" | PY.DECORATOR             |
      | input                | "file.txt" | PY.DECORATOR             |
      | params               | "file.txt" | PY.DECORATOR             |
      | log                  | "file.txt" | PY.DECORATOR             |
      | resources            | foo        | PY.DECORATOR             |
      | version              | ""         | PY.DECORATOR             |
      | message              | ""         | PY.DECORATOR             |
      | threads              | ""         | PY.DECORATOR             |
      | singularity          | ""         | PY.DECORATOR             |
      | priority             | ""         | PY.DECORATOR             |
      | benchmark            | ""         | PY.DECORATOR             |
      | wildcard_constraints | ""         | PY.DECORATOR             |
      | group                | ""         | PY.DECORATOR             |
      | shadow               | ""         | PY.DECORATOR             |
      | conda                | ""         | PY.DECORATOR             |
      | cwl                  | ""         | PY.DECORATOR             |
      | script               | ""         | PY.DECORATOR             |
      | shell                | ""         | PY.DECORATOR             |
      | run                  | ""         | PY.PREDEFINED_DEFINITION |
      | wrapper              | ""         | PY.DECORATOR             |
