package de.uros.citlab.errorrate.types;


import java.util.Arrays;
import java.util.Objects;

public class RecoRef {

    private String reco;
    private String ref;

    public RecoRef(String[] recos, String[] refs) {
        reco = recos == null || recos.length == 0 ? null : recos[0];
        ref = refs == null || refs.length == 0 ? null : refs[0];
        if (recos != null && recos.length > 1) {
            throw new RuntimeException("recos '" + Arrays.toString(recos) + "' has length>1");
        }
        if (refs != null && refs.length > 1) {
            throw new RuntimeException("refs '" + Arrays.toString(refs) + "' has length>1");
        }
    }

    public RecoRef(String reco, String ref) {
        this.reco = reco;
        this.ref = ref;
    }

    public String getReco() {
        return reco;
    }

    public String getRef() {
        return ref;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (reco == null ? 0 : reco.hashCode());
        hash = 29 * hash + (ref == null ? 0 : ref.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RecoRef other = (RecoRef) obj;
        return Objects.equals(this.reco, other.reco) && Objects.equals(this.ref, other.ref);
    }

}