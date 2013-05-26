uniform sampler2D lookupTexture;
uniform float theMax;

varying float varyingHeight;

void main(){
    gl_FragColor = texture2D(lookupTexture, vec2(varyingHeight / theMax,0.0));
}