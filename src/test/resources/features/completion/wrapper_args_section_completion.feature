Feature: Completion for arguments used in wrapper

  Scenario Outline: Simple completion for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <section>:
    And I invoke autocompletion popup
    Then completion list should contain:
      | <completion> |
    Examples:
      | rule_like   | section | wrapper               | completion |
      | rule       | input   | bio/bcftools/reheader | vcf        |
      | rule       | input   | bio/bwa/mem           | reads      |
      | rule       | params  | bio/gatk/applybqsr    | java_opts  |
      | checkpoint | params  | bio/bcftools/call     | mpileup    |
      | checkpoint | params  | utils/cairosvg        | extra      |
      | checkpoint | output  | bio/last/lastal       | blasttab   |

  Scenario Outline: No completion for bundled wrappers in wrong context
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:  <text>
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret at <signature>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <completion> |
    Examples:
      | rule_like | section | text      | signature | wrapper        | completion |
      | rule      | input   | "#here" | #here     | bio/bcftools/reheader | vcf        |
      | rule      | params  | a=#here   | #here     | utils/cairosvg | extra      |
      | rule      | params  | a=here    | here      | utils/cairosvg | extra      |
      | rule      | params  | foo(here) | here      | utils/cairosvg | extra      |
