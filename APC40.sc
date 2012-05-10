

APC40 {

	var ccr,nonr,noffr,apc,src,handlers,map,noteMap,noteOffMap,<clipLaunchStates;
	var matchClip, matchScene;

	*new { arg install=true;
		if(MIDIClient.initialized.not,{MIDIIn.connectAll});
		^super.new.init(install);
	}

	// receiving
	fader { arg i,func; // arg 0..127, faderi
		handlers.put('fader',i ? '*',
			{ arg value,num,chan; func.value(value,chan) })
	}
	master { arg func; // arg 0..127
		handlers.put('master','*',func)
	}
	xfader { arg func;
		handlers.put('xfader','*',func)
	}
	trackControl { arg i,func;
		// the top right 8 knobs
		handlers.put('trackControl',i ? '*',
			{ arg value,num,chan; func.value(value,chan) })
	}
	deviceControl { arg tracki,knobi,func; 
		// func: arg val,knob,track
		// bottom right 8 knobs
		// context dependent on which track is selected
		// you may supply track=nil to just use the 8 without regard to track selection
		// however the bank recalls previous values for the track
		// when it is selected so you will lose your place
		handlers.put('deviceControl',tracki,knobi ? '*',
			{ arg value,num,chan; 
				func.value(value,num - 16,chan) 
			})
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
			note = matchClip;
		},{
			note = button + 53;
		});
		noteMap.put([track,note], MIDIEvent(nil, src, track, note, state) -> f );
	}
	sceneLaunch { arg row,func;
		this.prAddNote(0,row ? matchScene,func)
	}
	clipStop { arg track,func;
		this.prAddNote(track,52,{ arg chan,note; func.value(chan) })
	}
	stopAllClips { arg func,offFunc;
		this.prAddNoteOnOff(0,81,{ arg chan,note; func.value() },offFunc);
	}
	/*trackSelect { arg i,func;
		// it sends all deviceControl knobs cc for the newly selected track
		// but otherwise no way to detect that trackSelect was pushed
		// unless detect the splurge if cc 16 .. 23
		handlers.put('trackSelect',i ? '*',func)
	}*/	
	activator { arg track,func;
		this.prAddNoteOnOff(track,50,
			{ arg chan,note; func.value(track,true) },
			{ arg chan,note; func.value(track,false) })
	}
	solo { arg track,func;
		this.prAddNoteOnOff(track,49,
			{ arg chan,note; func.value(track,true) },
			{ arg chan,note; func.value(track,false) })
	}
	recordArm { arg track,func;
		this.prAddNoteOnOff(track,48,
			{ arg chan,note; func.value(track,true) },
			{ arg chan,note; func.value(track,false) })
	}
	play { arg func;
		this.prAddNote(0,91,{ arg chan,note; func.value() })
	}
	stop { arg func;
		this.prAddNote(0,92,{ arg chan,note; func.value() })
	}
	rec { arg func;
		this.prAddNote(0,93,{ arg chan,note; func.value() })
	}
	// bankSelect
	nudgeMinus { arg func;
		this.prAddNote(0,101,{ arg chan,note; func.value() })
	}
	nudgePlus { arg func;
		this.prAddNote(0,100,{ arg chan,note; func.value() })
	}
	tapTempo { arg func;
		this.prAddNote(0,99,{ arg chan,note; func.value() })
	}
	// 8 click buttons below, 4 above


	// blinkenlichten
	setClip { arg track,button,mode;
		/* set the clipLaunch button blinking
		button: 0..4 = top..bottom
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

	// private
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
		noffr = NoteOffResponder({ arg src,chan,num,value;
				noteOffMap.detect { arg mev;
					if(mev.key.match(src, chan, num, nil),{
						// if it matches only on a specific state
						// then it may return false
						mev.value.value(src,chan,num,value);
					},{
						false
					})
				}
			},src);
	}
	remove {
		ccr.remove;
		ccr = nil;
		nonr.remove;
		nonr = nil;
		noffr.remove;
		noffr = nil;
	}
	init { arg add;
		apc = MIDIOut.newByName("Akai APC40", "Akai APC40");
		src = MIDIClient.sources.detect({ arg midiEndPoint; 
			midiEndPoint.name == "Akai APC40" }).uid;

		clipLaunchStates = Array2D.fromArray(5,8,0!40);
		handlers = MultiLevelIdentityDictionary.new;
		map = MultiLevelIdentityDictionary.new;
		noteMap = Dictionary.new;
		noteOffMap = Dictionary.new;
		matchClip = { arg b; b.inclusivelyBetween(53,61) };
		matchScene = { arg b; b.inclusivelyBetween(82,86) };
		8.do { arg i;
			map.put(i,7,[\fader,i]);
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
	prAddNote { arg track,note,func;
		var f;
		f = { arg src,chan,note,velocity;
			func.value(chan,note);
			true
		};
		noteMap.put([track,note], MIDIEvent(nil,src,track,note) -> f );
	}
	prAddNoteOff { arg track,note,func;
		var f;
		f = { arg src,chan,note,velocity;
			func.value(chan,note);
			true
		};
		noteOffMap.put([track,note], MIDIEvent(nil,src,track,note) -> f )
	}
	prAddNoteOnOff { arg track,note,onFunc,offFunc;
		this.prAddNote(track,note,onFunc);
		if(offFunc.notNil,{
			this.prAddNoteOff(track,note,offFunc)
		})
	}
}



