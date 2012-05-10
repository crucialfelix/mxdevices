
var cs;
cs = ControlSpec(0,127,\lin,1);

MxUnit.register('APC40',
(
	make: { arg apc;
		var inlets,outlets,out;
		~source = apc;
		inlets = [
		  MxInlet("setAllClips",0,
			ArraySpec(ControlSpec(0,5,step:1)),
			MxSetter({ arg array; apc.setAllClips(array) })
		  )
		];
		outlets = List.new;
		8.do { arg chani;
			var out;
		  	out = MxOutlet("fader"+chani,
		  					0 + chani,
							cs,
							MxHasAction({arg f; apc.fader(chani,{ arg i,val;
									f.value(val)
								}) 
							}));
		  	outlets.add(out);
		};
		out = MxOutlet("master",
		  					8,
							cs,
							MxHasAction({arg f; apc.master(f)
							}));
		outlets.add(out);
		out = MxOutlet("xfader",
		  					9,
							cs,
							MxHasAction({arg f; apc.xfader(f)
							}));
		outlets.add(out);
		8.do { arg chani;
			var out;
		  	out = MxOutlet("trackControl"+chani,
		  					10 + chani,
							cs,
							MxHasAction({arg f; apc.trackControl(chani,{ arg i,val;
									f.value(val)
								}) 
							}));
		  	outlets.add(out);
		};

		MxUnit(apc,inlets,outlets)
	}
));