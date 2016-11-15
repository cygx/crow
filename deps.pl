use strict;
use warnings;
use File::Find;

my $sep = $^O =~ /win32|cygwin/i ? ';' : ':';
my %classes;
my @classes;

sub gather {
    return unless -f && s/\.java$//;
    die "$_ already not unique\n"
        if exists $classes{$_};

    my $class = $File::Find::name;
    $class =~ s/^sources/classes/;
    $class =~ s/java$/class/;

    $classes{$_} = {
        name => $_,
        src => $File::Find::name,
        class => $class,
        deps => {},
    };
}

sub mkdeps {
    for my $c (@classes) {
        open my $fh, '<', $c->{src};
        my %words;
        @words{/\b\w+\b/g} = () while <$fh>;
        close $fh;

        for my $w (keys %words) {
            $c->{deps}->{$w} = undef
                if exists $classes{$w};
        }

        delete $c->{deps}->{$c->{name}};
        $c->{deps} = [ sort keys %{$c->{deps}} ];
    }
}

find \&gather, 'sources';
@classes = @classes{sort keys %classes};
mkdeps;

print $_->{name}, ': ', $_->{class}, "\n"
    for @classes;

print "\n.classes.dummy:", map({ ' '.$_->{src} } @classes),
    "\n\tjavac -d classes -sourcepath sources \$(filter %.java,\$^)",
    "\n\t\@touch \$@\n";

for (@classes) {
    print "\n", $_->{class}, ":", map({ ' '.$classes{$_}->{src} } @{$_->{deps}})
        if @{$_->{deps}};
}

print "\n\n", join(' ', map { $_->{class} } @classes),
    ": classes/%.class: sources/%.java",
    "\n\tjavac -d classes -sourcepath sources \$<\n",
    map({ ' '.$classes{$_}->{src} } @{$_->{deps}});

%classes = ();
find \&gather, 'tests';
@classes = @classes{sort keys %classes};

print "\nprove: .tests.dummy\n\tprove -e\$(MAKE)",
    map({ ' '.$_->{name} } @classes), "\n";

print "\nprove-v: .tests.dummy\n\tprove -v -e\$(MAKE)",
    map({ ' '.$_->{name} } @classes), "\n";

print "\nrun-tests: .tests.dummy\n",
    map({ "\tjava -cp 'classes${sep}tests' -ea ".$_->{name}."\n" } @classes);

print "\n", join(' ', map({ $_->{name} } @classes)),
    ": %: tests/%.class .classes.dummy",
    "\n\tjava -cp 'classes${sep}tests' -ea \$@\n";

print "\n.tests.dummy: .classes.dummy", map({ ' '.$_->{src} } @classes),
    "\n\tjavac -cp classes \$(filter %.java,\$^)",
    "\n\t\@touch \$@\n";

print "\n", join(' ', map({ $_->{class} } @classes)),
    ": %.class: %.java .classes.dummy",
    "\n\tjavac -cp 'classes${sep}tests' \$<\n";
