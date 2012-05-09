

APC40 {

	var ccr,nonr,noffr,apc,src,handlers,map,noteMap,<clipLaunchStates;

	*new { arg install=true;
		if(MIDIClient.initialized.not,{MIDIIn.connectAll});
		^super.new.init(install);
	}

	// receiving
	fader { arg i,func;
		this.put('fader',i ? '*',func)
	}
	master { arg func;
		this.put('master','*',func)
	}
	xfader { arg func;
		this.put('xfader','*',func)
	}
	//trackSelect { arg i,func;
		// it sends all deviceControl knobs cc for the newly selected track
		// but otherwise no way to detect that trackSelect was pushed
		// unless detect the splurge if cc 16 .. 23
	//	this.put('trackSelect',i ? '*',func)
	//}
	trackControl { arg i,func;
		// the top right 8 knobs
		this.put('trackControl',i ? '*',func)
	}
	deviceControl { arg track,i,func;
		// bottom right 8 knobs
		// context dependent on which track is selected
		// you may supply track=nil to just use the 8 without regard to track selection
		// however the bank recalls previous values for the track
		// when it is selected so you will lose your place
		this.put('deviceControl',track,i ? '*',func)
	}
	clipLaunch { arg func,track,button,state,blinkCycle=#[0,1];
		var f,note;
		if(blinkCycle.notNil,{
			f = { arg src, chan,num,veloc;
				var currentState,newState,bi;
				bi = num - 53;
				currentState = clipLaunchStates.at(bi,chan);
				newState = blinkCycle.wrapAt( blinkCycle.indexOf(currentState) + 1 );
				this.setClip(chan,bi,newState);
				if(state.isNil or: {newState == state},{
					func.value(chan,bi,newState);
					true
				},false)
			};
		},{
			f =  { arg src, chan,num,veloc;
				num = num - 53;
				func.value(chan,num,clipLaunchStates.at(num,chan));
				true
			};
		});
		if(button.isNil,{
			note = { arg b; b.inclusivelyBetween(53,61) };
		},{
			note = button + 53;
		});
		noteMap = noteMap.add( MIDIEvent(nil, src, track, note, state) -> f );
	}
	// noteOff too
	// sceneLaunch
	// clipStop
	// stopAllClips
	// activator
	// solo
	// recordArm
	// play
	// stop
	// rec

	// blinkenlichten
	setClip { arg track,button,mode;
		/* set the clipLaunch button blinking
		button: top - bottom
		mode:
			0 - Off
			1 - Green
			2 - Green Flashing
			3 - Red
			4 - Red Flashing
			5 - Orange
			6 - Orange Flashing
		*/
		apc.write(4,144+track,0+track,53+button,mode);
		clipLaunchStates.put(button,track,mode);
	}
	setAllClips { arg array2d;
		// default: set all to off
		// Array2D is row, col
		array2d = array2d ?? {Array2D.fromArray(5,8,0!40)};
		array2d.cols.do { arg track;
			array2d.rows.do { arg button;
				this.setClip(track,button,array2d.at(button,track))
			}
		}
	}

	//
	add {
		ccr = CCResponder({ arg src,chan,num,value;
				var key,handler;
				key = map.at(chan,num);
				if(key.notNil,{
					handler = handlers.at(*key) ?? {
						key.put(1,'*'); // wildcard number
						handlers.at(*key)
					};
					handler.value(value);
				})
			},src);
		nonr = NoteOnResponder({ arg src,chan,num,value;
				noteMap.detect { arg mev;
					if(mev.key.match(src, chan, num, nil),{
						// if it matches only on a specific state
						// then it may return false
						mev.value.value(src,chan,num,value);
					},{
						false
					})
				}
			},src);
		//noffr
	}
	remove {
		ccr.remove;
		ccr = nil;
		nonr.remove;
		nonr = nil;
	}
	init { arg add;
		apc = MIDIOut.newByName("Akai APC40", "Akai APC40");
		src = MIDIClient.sources.detect({ arg midiEndPoint; 
			midiEndPoint.name == "Akai APC40" }).uid;

		clipLaunchStates = Array2D.fromArray(5,8,0!40);
		handlers = MultiLevelIdentityDictionary.new;
		map = MultiLevelIdentityDictionary.new;
		8.do { arg i;
			map.put(i,7,[\fader,i]);

			// map.put(i,16,['trackSelect',i]);
			
			map.put(0,48 + i,['trackControl',i]);

			8.do { arg controli;
				map.put(i,16 + controli,['deviceControl',i,controli]);
			}
		};
		map.put(0,14,['master',0]);
		map.put(0,15,['xfader',0]);
		8.do { arg controli;
			map.put(8,16 + controli,['deviceControl',8,controli]);
		};
		if(add,{this.add})
	}
	put { arg key,index,func;
		handlers.put(key,index,func)
	}
}

/*
APC40r {
	
	var <>func,<>name,<>index;
		
	value { arg val;	
		func.value(val,index)
	}
}
*/

/*
	subclass CCBank or use one
		
	outlets:
		fader0
			...
			
		master
		xfader
		
		track0
			...

		
		trackControl1
			...
		deviceControl1
			...
			
	find src number by name in devices
	
		
*/