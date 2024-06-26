/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package joe.schwarz.webcam;

import org.bridj.BridJ;
import org.bridj.Platform;
import org.bridj.Pointer;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Runtime;
import org.bridj.cpp.CPPObject;
import org.bridj.cpp.CPPRuntime;


/**
 * <i>native declaration : line 1</i><br>
 * This file was autogenerated by <a
 * href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that
 * <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a
 * few opensource projects.</a>.<br>
 * For help, please visit <a
 * href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a
 * href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("OpenIMAJGrabber")
@Runtime(CPPRuntime.class)
@SuppressWarnings("all")
public class OpenIMAJGrabber extends CPPObject {

	static {
		Platform.addEmbeddedLibraryResourceRoot("JFX/Webcam/lib/");
		BridJ.register();
	}

	public OpenIMAJGrabber() {
		super();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public OpenIMAJGrabber(Pointer pointer) {
		super(pointer);
	}

	public native Pointer<DeviceList> getVideoDevices();

	public native Pointer<Byte> getImage();

	public native int nextFrame();

	public native void setTimeout(int timeout);

	public native boolean startSession(int width, int height, double reqFPS);

	public native boolean startSession(int width, int height, double reqFPS, Pointer<Device> devptr);

	public native void stopSession();

	public native int getWidth();

	public native int getHeight();

	// / C type : void*
	@Field(0)
	protected Pointer<?> data() {
		return this.io.getPointerField(this, 0);
	}

	// / C type : void*
	@Field(0)
	protected OpenIMAJGrabber data(Pointer<?> data) {
		this.io.setPointerField(this, 0, data);
		return this;
	}
}
