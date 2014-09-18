
var cs;
cs = ControlSpec(0, 127, \lin, 1);

MxUnit.register('APC40',
(
	make: { arg apc;
		var inlets, outlets, out, i=0;
		~source = apc;
		inlets = [
		  MxInlet("setAllClips", 0,
			ArraySpec(ControlSpec(0, 5, step:1)),
			MxSetter({ arg array; apc.setAllClips(array) })
		  )
		];
		outlets = List.new;
		8.do { arg chani;
			var out;
			i = i + chani;
		  	out = MxOutlet("fader"++(chani + 1),
		  					i,
							cs,
							MxHasAction({arg f; apc.fader(chani, { arg val, chani;
									f.value(val)
								})
							}));
		  	outlets.add(out);
		};
		i = i + 1;
		out = MxOutlet("master",
		  					i,
							cs,
							MxHasAction({arg f; apc.master(f)
							}));
		outlets.add(out);
		i = i + 1;
		out = MxOutlet("xfader",
		  					i,
							cs,
							MxHasAction({arg f; apc.xfader(f)
							}));
		outlets.add(out);
		8.do { arg chani;
			var out;
			i = i + 1;
		  	out = MxOutlet("trackControl"++(chani+1),
		  					i,
							cs,
							MxHasAction({arg f; apc.trackControl(chani, { arg val, chani;
									f.value(val)
								})
							}));
		  	outlets.add(out);
		};
		8.do { arg chani;
			8.do { arg knobi;
				var out;
				i = i + 1;
			  	out = MxOutlet("deviceControl"++(chani+1)++(knobi+1),
			  					i,
								cs,
								MxHasAction({arg f; apc.deviceControl(chani, knobi, { arg val, knobi, chani;
										f.value(val)
									})
								}));
			  	outlets.add(out);
			}
		};

		MxUnit(apc, inlets, outlets)
	}
));