Feature: Completion string literal options in sections with fixed completion variants
  E.g. shadow, template_engine

  Scenario Outline: Complete string literal in sections with fixed options set
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
      <section>: <quote><quote>
    """
    When I put the caret after <section>: <quote>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <option> |
    Examples:
      | section         | quote | option       |
      | shadow          | '     | full         |
      | shadow          | "     | full         |
      | shadow          | "     | copy-minimal |
      | shadow          | "     | minimal      |
      | shadow          | "     | shallow      |
      | template_engine | '     | jinja2       |
      | template_engine | "     | yte          |

  Scenario Outline: Check insertion after autocompletion
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
      <section>: ""
    """
    When I put the caret after <section>: "
    Then I invoke autocompletion popup, select "<option>" lookup item and see a text:
    """
    rule a:
      <section>: "<option>"
    """
    Examples:
      | section         | option |
      | shadow          | full   |
      | template_engine | jinja2 |

  Scenario Outline: Check completion outside string literal
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
      <section>: ""
    """
    When I put the caret after <section>:
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <option> |
    Examples:
      | section         | option       |
      | shadow          | copy-minimal |
      | shadow          | shallow      |
      | template_engine | jinja2       |
